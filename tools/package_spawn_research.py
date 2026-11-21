#!/usr/bin/env python3
"""Build the read-only research workbook and download bundle (requires openpyxl)."""
from pathlib import Path
import argparse,csv,json,zipfile,hashlib
from openpyxl import Workbook,load_workbook
from openpyxl.styles import Font,PatternFill,Alignment
from openpyxl.utils import get_column_letter
p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path('docs/pokemon-sales-research'));a=p.parse_args();root=a.root;data=root/'data'
summary=list(csv.DictReader((data/'pokemon-resumo.csv').open(encoding='utf-8-sig'),delimiter=';'))
labels={'comum':'Comum — rota observada','comum_condicionado':'Comum com requisitos adicionais','incomum':'Incomum — rota observada','raro':'Raro — rota observada','ultrarraro':'Ultra-raro — rota observada','rotas_dependentes_ou_nao_resolvidas':'Rotas dependentes de addon ou tag não resolvida','sem_rota_neste_pool':'Sem rota neste pool (revisão manual)','sem_implemented_true':'Sem implemented=true no registro'}
lines=['# Lista por grupo observado','','Cobblemon Fabric 1.8.3. Agrupamento para pesquisa; não representa porcentagem global nem preço aprovado. Condições, tags não resolvidas e outros buckets estão nos CSVs.','']
for key,label in labels.items():
 selected=[r for r in summary if r['categoria_observada']==key];lines+=['## '+label+' — '+str(len(selected)),'','| Dex | Pokémon | Catch rate base | Métodos normais observados |','| ---: | --- | ---: | --- |']
 lines += [f"| {r['dex']} | {r['nome']} | {r['catch_rate_base']} | {r['metodos'] or '—'} |" for r in selected];lines+=['']
(root/'lista-por-grupo.md').write_text('\n'.join(lines)+'\n')
metadata=json.loads((data/'metadata.json').read_text());w=Workbook();intro=w.active;intro.title='Leia primeiro'
notes=[['Pesquisa oficial','Cobblemon Fabric 1.8.3+1.21.1'],['Espécies registradas',metadata['species_definitions']],['Marcadas implemented=true',metadata['species_implemented_true']],['Arquivos de pool',metadata['spawn_pool_files']],['Registros normalizados',metadata['flattened_species_rows']],['Peso não é porcentagem','Não some pesos de contextos diferentes'],['Grupos/herds','Peso do grupo e peso do membro são campos distintos'],['Categoria observada','Agrupamento de pesquisa, não preço aprovado'],['Tags próprias não definidas',len(metadata['undefined_cobblemon_tags'])],['Registros que exigem tais tags',metadata['rows_requiring_undefined_cobblemon_tag']],['Proveniência','Modrinth IgUZQKm8; SHA-512 verificado'],['Limites','Não contém datapacks/configuração real do servidor'],['Preços','Ainda não definidos; proposta em README.md']]
for row in notes:intro.append(row)
intro.column_dimensions['A'].width=35;intro.column_dimensions['B'].width=82
for name,file in [('Resumo','pokemon-resumo.csv'),('Pesca','rotas-pesca.csv'),('Todas as rotas','rotas-spawn.csv')]:
 sheet=w.create_sheet(name);rows=list(csv.reader((data/file).open(encoding='utf-8-sig'),delimiter=';'));headers=rows[0];sheet.append(headers)
 for row in rows[1:]:
  converted=[]
  for key,value in zip(headers,row):
   if value in ['True','False']:value=value=='True'
   elif value and key in {'dex','catch_rate_base','strings_propriedades_distintas','entradas_individuais','registros_membro_grupo_normal','registros_grupo_boss','max_grupo','max_membro'}:value=int(value)
   elif value and key in {'peso_entrada','peso_membro'}:value=float(value)
   converted.append(value)
  sheet.append(converted)
 sheet.freeze_panes='D2';sheet.auto_filter.ref=sheet.dimensions
 for cell in sheet[1]:cell.fill=PatternFill('solid',fgColor='174657');cell.font=Font(color='FFFFFF',bold=True);cell.alignment=Alignment(wrap_text=True)
 sheet.row_dimensions[1].height=42
 for column in range(1,len(rows[0])+1):sheet.column_dimensions[get_column_letter(column)].width=24 if column>1 else 21
b=w.create_sheet('Pesos dos grupos');b.append(['Contexto','Grupo','Peso padrão (não chance da espécie)'])
for context,groups in metadata['default_bucket_weights'].items():
 for group,value in groups.items():b.append([context,group,value])
b.column_dimensions['A'].width=30;b.column_dimensions['B'].width=20;b.column_dimensions['C'].width=40;b.freeze_panes='A2'
missing=w.create_sheet('Tags não definidas');missing.append(['Tag própria sem definição encontrada no JAR'])
for tag in metadata['undefined_cobblemon_tags']:missing.append([tag])
missing.column_dimensions['A'].width=65
xlsx=root/'pesquisa-spawns-1.8.3.xlsx';w.save(xlsx);check=load_workbook(xlsx,read_only=True);assert check['Resumo'].max_row==1026 and check['Todas as rotas'].max_row==8717;check.close()
archive=root/'pesquisa-spawns-cobblemon-1.8.3.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for path in [root/'README.md',root/'lista-por-grupo.md',xlsx]+sorted(data.glob('*')):z.write(path,str(path.relative_to(root)))
(root/'SHA256SUMS').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in [xlsx,archive]))
print(xlsx);print(archive)
