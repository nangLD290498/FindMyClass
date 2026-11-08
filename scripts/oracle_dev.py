#!/usr/bin/env python3
"""Manage the local development Oracle database without printing credentials."""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local' / 'oracle'
CONTAINER = 'findmyclass-oracle'
VOLUME = 'findmyclass-oracle-data'
IMAGE = 'gvenzl/oracle-free:23-slim'
DOCKER = ['docker', '--host=unix:///var/run/docker.sock']


def docker(*args, **kwargs):
    # Always use the managed local daemon, even if the caller has another context.
    env = os.environ.copy()
    for name in ['DOCKER_HOST', 'DOCKER_CONTEXT', 'DOCKER_TLS', 'DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH']:
        env.pop(name, None)
    return subprocess.run(DOCKER + list(args), env=env, check=True, **kwargs)


def private_file(path, text):
    with open(path, 'x', opener=lambda name, flags: os.open(name, flags, 0o600)) as f:
        f.write(text)


def credentials():
    return json.loads((STATE / 'credentials.json').read_text())


def create_credentials():
    STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    path = STATE / 'credentials.json'
    if not path.exists():
        # Alphanumeric passwords avoid SQL/shell quoting ambiguities; never log them.
        password = lambda: 'Fmc' + secrets.token_hex(12)
        private_file(path, json.dumps({'admin': password(), 'app': password(), 'test': password()}))
    values = credentials()
    env_file = STATE / 'container.env'
    if not env_file.exists():
        private_file(env_file, 'ORACLE_PASSWORD=' + values['admin'] + '\nAPP_USER=findmyclass\nAPP_USER_PASSWORD=' + values['app'] + '\n')


def info():
    result = docker('inspect', CONTAINER, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return json.loads(result.stdout)[0]


def start():
    create_credentials()
    names = docker('ps', '-a', '--format', '{{.Names}}', stdout=subprocess.PIPE, text=True).stdout.splitlines()
    if CONTAINER in names:
        if info().get('Config', {}).get('Labels', {}).get('com.findmyclass.purpose') != 'development-oracle':
            raise RuntimeError('The container name belongs to another setup; leaving it unchanged.')
        docker('start', CONTAINER, stdout=subprocess.DEVNULL)
    else:
        image_pin = STATE / 'image.txt'
        if not image_pin.exists():
            docker('pull', IMAGE)
            metadata = docker('image', 'inspect', IMAGE, '--format', '{{json .RepoDigests}}', stdout=subprocess.PIPE, text=True)
            digest = json.loads(metadata.stdout)[0]
            private_file(image_pin, digest + '\n')
        docker('run', '-d', '--name', CONTAINER,
               '--label', 'com.findmyclass.purpose=development-oracle',
               '--restart', 'unless-stopped', '--memory', '4g', '--cpus', '2', '--shm-size', '1g',
               '--publish', '127.0.0.1:1521:1521',
               '--env-file', str(STATE / 'container.env'),
               '--mount', 'type=volume,source=' + VOLUME + ',target=/opt/oracle/oradata',
               '--health-cmd', 'healthcheck.sh', '--health-interval', '10s',
               '--health-timeout', '5s', '--health-retries', '30', '--health-start-period', '60s',
               image_pin.read_text().strip(), stdout=subprocess.DEVNULL)
    print('Oracle started. Run: python3 scripts/oracle_dev.py status')


def status():
    container = info()
    state = container['State']
    print('Container:', CONTAINER)
    print('Status:', state['Status'])
    print('Health:', state.get('Health', {}).get('Status', 'unknown'))
    print('JDBC URL: jdbc:oracle:thin:@localhost:1521/FREEPDB1')
    print('Application schema: findmyclass; test schema: findmyclass_test')
    print('Credentials: .local/oracle/credentials.json (local only; do not commit)')


def init_tests():
    if info()['State'].get('Health', {}).get('Status') != 'healthy':
        raise RuntimeError('Oracle is not healthy yet. Check status before initializing the test schema.')
    # SQL input and output can contain passwords; keep both out of terminal/log output.
    password = credentials()['test']
    sql = '''whenever sqlerror exit sql.sqlcode
set echo off verify off feedback off heading off
alter session set container=FREEPDB1;
declare
  user_count number;
begin
  select count(*) into user_count from dba_users where username = 'FINDMYCLASS_TEST';
  if user_count = 0 then
    execute immediate 'create user findmyclass_test identified by "''' + password + '''"';
  end if;
end;
/
grant create session, create table, create sequence to findmyclass_test;
alter user findmyclass_test quota 200M on users;
exit
'''
    result = docker('exec', '-i', CONTAINER, 'sqlplus', '-s', '/', 'as', 'sysdba',
                    input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    print('Dedicated Oracle test schema is ready.')


def run(command, use_test):
    if not command:
        raise RuntimeError('Supply a command after --, such as ./backend/mvnw -f backend/pom.xml spring-boot:run')
    if command[0] == '--': command = command[1:]
    values = credentials()
    env = os.environ.copy()
    env.update(DB_URL='jdbc:oracle:thin:@localhost:1521/FREEPDB1',
               DB_USERNAME='findmyclass_test' if use_test else 'findmyclass',
               DB_PASSWORD=values['test' if use_test else 'app'])
    if use_test:
        # Test-profile datasource values are overridden only for this child process.
        settings = json.loads(env.get('SPRING_APPLICATION_JSON', '{}'))
        datasource = settings.setdefault('spring', {}).setdefault('datasource', {})
        datasource.update(url=env['DB_URL'], username=env['DB_USERNAME'],
                          password=env['DB_PASSWORD'], **{'driver-class-name': 'oracle.jdbc.OracleDriver'})
        env['SPRING_APPLICATION_JSON'] = json.dumps(settings)
    return subprocess.call(command, cwd=ROOT, env=env)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'status', 'stop', 'init-tests', 'run', 'test'])
    parser.add_argument('command', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.action == 'start': start()
    elif args.action == 'status': status()
    elif args.action == 'stop':
        docker('stop', CONTAINER, stdout=subprocess.DEVNULL)
        print('Oracle stopped; the data volume and credentials are retained.')
    elif args.action == 'init-tests': init_tests()
    else: return run(args.command, args.action == 'test')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except subprocess.CalledProcessError as error:
        # Captured Docker/SQL output may contain credentials. Do not echo it.
        print('Oracle operation failed (exit ' + str(error.returncode) + '). Check container health and local configuration.', file=sys.stderr)
        sys.exit(1)
    except (RuntimeError, FileNotFoundError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
