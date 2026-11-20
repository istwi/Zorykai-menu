#!/usr/bin/env python3
"""Inicia o runtime isolado, respeitando o proxy HTTPS herdado e TLS padrão."""
import argparse
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('runtime', type=Path)
parser.add_argument('--integration-test', action='store_true')
args = parser.parse_args()
base = args.runtime.resolve()
if 'eula=true' not in (base / 'eula.txt').read_text().splitlines():
    raise SystemExit('Aceitação explícita da EULA necessária no runtime local.')
command = ['java', '-Xms512M', '-Xmx3G', '-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
if args.integration_test:
    if 'server-ip=127.0.0.1' not in (base / 'server.properties').read_text().splitlines():
        raise SystemExit('O ensaio exige servidor isolado ligado somente ao loopback.')
    command += ['-Dzorykai.integrationTest=true']
for scheme in ['http', 'https']:
    proxy = os.environ.get(scheme.upper() + '_PROXY') or os.environ.get(scheme + '_proxy')
    if proxy:
        parsed = urlsplit(proxy)
        if not parsed.hostname:
            raise SystemExit('Proxy herdado inválido')
        if parsed.username or parsed.password:
            raise SystemExit('Proxy com autenticação exige configuração específica; valores não serão expostos.')
        command += [f'-D{scheme}.proxyHost={parsed.hostname}',
                    f'-D{scheme}.proxyPort={parsed.port or 80}']
command += ['-Dhttp.nonProxyHosts=localhost|127.*|[::1]',
            '-Dfabric.gameJarPath=' + str(base / 'libs/server-1.21.1.jar'),
            '-cp', str(base / 'libs/*'), 'net.fabricmc.loader.impl.launch.knot.KnotServer', 'nogui']
with (base / 'zorykai-server.log').open('w') as log:
    child = subprocess.Popen(command, cwd=base, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    try:
        for line in child.stdout:
            log.write(line)
            log.flush()
            sys.stdout.write(line)
            sys.stdout.flush()
        raise SystemExit(child.wait())
    except KeyboardInterrupt:
        child.terminate()
        raise SystemExit(child.wait())
