#!/usr/bin/env python3
"""Original factual inventory of official Cobblemon spawning data; not a probability simulator."""
import argparse,collections,csv,hashlib,json,re,zipfile
from pathlib import Path

OFFICIAL_SHA512='0cfcbc811c0434a6a8cbeda32fae5c39328a699e28b4292de635da99fc283f40166d25055594abf4eff56dd9a7a453313a5aa43f4a9969940d9e1a18dd222166'
BUCKETS=['common','uncommon','rare','ultra-rare']
BUCKET_PT={'common':'comum','uncommon':'incomum','rare':'raro','ultra-rare':'ultrarraro','boss':'grupo_boss_alpha'}
p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--out',type=Path,default=Path('docs/pokemon-sales-research/data'));a=p.parse_args()
raw=a.jar.read_bytes()
if hashlib.sha512(raw).hexdigest()!=OFFICIAL_SHA512:raise SystemExit('Use the verified official Fabric 1.8.3+1.21.1 JAR; hash differs.')
z=zipfile.ZipFile(a.jar);archive_names=set(z.namelist());manifest=json.loads(z.read('fabric.mod.json'));assert manifest['version']=='1.8.3+1.21.1'
load=lambda n:json.loads(z.read(n))
species={n.rsplit('/',1)[-1][:-5]:(load(n),n) for n in z.namelist() if n.startswith('data/cobblemon/species/') and n.endswith('.json')}
presets={n.rsplit('/',1)[-1][:-5]:load(n) for n in z.namelist() if n.startswith('data/cobblemon/spawn_detail_presets/') and n.endswith('.json')}
config=load('data/cobblemon/spawning/best-spawner-config.json')
rows=[];files=0;parent_types=collections.Counter();parent_buckets=collections.Counter();disabled=0;empty=[]
def encoded(v):return json.dumps(v,ensure_ascii=False,separators=(',',':'))
def external_only(conditions):
 for key in ['biomes','structures','neededBaseBlocks','neededNearbyBlocks']:
  alternatives=conditions.get(key,[])
  if alternatives and all(isinstance(v,str) and ':' in v and v.lstrip('#').split(':')[0] not in {'minecraft','cobblemon'} for v in alternatives):return True
 return False
def missing_own_tag(value,kind):
 if not isinstance(value,str) or not value.startswith('#cobblemon:'):return False
 namespace,path=value[1:].split(':',1)
 return f'data/{namespace}/tags/{kind}/{path}.json' not in archive_names
def unresolved_requirements(conditions):
 missing=[];required=False
 for field,kind in [('biomes','worldgen/biome'),('structures','worldgen/structure'),('neededNearbyBlocks','block'),('neededBaseBlocks','block')]:
  choices=conditions.get(field,[])
  missing.extend(v for v in choices if missing_own_tag(v,kind))
  if choices and all(missing_own_tag(v,kind) for v in choices):required=True
 return sorted(set(missing)),required
def flags_for(s):
 c=s.get('condition',{});flags=[]
 for key in ['isSlimeChunk','isPokeSnack','structures','neededNearbyBlocks','neededBaseBlocks','minLureLevel','maxLureLevel','fishingBait','neededNearbyFluids']:
  if key in c:flags.append(key)
 flags.extend('preset:'+n for n in s.get('presets',[]) if n not in {'natural','wild','water','foliage','treetop'})
 if any(k in c for k in ['minY','maxY']):flags.append('altura')
 if any(k in c for k in ['timeRange','isRaining','isThundering','moonPhase']):flags.append('tempo_clima')
 return sorted(set(flags))
for path in sorted(z.namelist()):
 if not path.startswith('data/cobblemon/spawn_pool_world/') or not path.endswith('.json'):continue
 files+=1;doc=load(path)
 if not doc.get('enabled',True):disabled+=1;continue
 for s in doc.get('spawns',[]):
  parent_types[s.get('type','unknown')]+=1;parent_buckets[s.get('bucket','unknown')]+=1
  if s.get('type')=='empty':empty.append({'file':path,'id':s.get('id'),'bucket':s.get('bucket'),'weight':s.get('weight')});continue
  is_herd=s.get('type')=='pokemon-herd'
  members=s.get('herdablePokemon',[]) if is_herd else [s]
  for member in members:
   props=member.get('pokemon','');sp=props.split(' ')[0].removeprefix('cobblemon:')
   if not props:continue
   if sp not in species:raise ValueError('Unknown species '+sp)
   required=doc.get('neededInstalledMods',[]);addon=external_only(s.get('condition',{})) or any(m not in {'minecraft','cobblemon','fabricloader','fabric-api'} for m in required)
   preset_data={n:presets.get(n,{'unknown':True}) for n in s.get('presets',[])}
   if any(external_only(v.get('condition',{})) for v in preset_data.values()):addon=True
   missing_tags,needs_missing=unresolved_requirements(s.get('condition',{}))
   for preset in preset_data.values():
    tags,required=unresolved_requirements(preset.get('condition',{}));missing_tags.extend(tags);needs_missing=needs_missing or required
   rows.append({
    'pokemon':sp,'propriedades':props,'bucket':s.get('bucket',''),'metodo':s.get('spawnablePositionType',s.get('context','')),
    'tipo_entrada':s.get('type',''),'papel_no_grupo':('lider' if member.get('isLeader') else 'seguidor' if member.get('isFollower') else 'membro') if is_herd else 'individual',
    'peso_entrada':s.get('weight',''),'peso_membro':member.get('weight','') if is_herd else '',
    'nivel':member.get('level',member.get('levelRange',s.get('level',s.get('levelRange','')))),
    'max_grupo':s.get('maxHerdSize',''),'max_membro':member.get('maxTimes','') if is_herd else '',
    'mod_externo_explicito':addon,'requer_tag_cobblemon_sem_definicao_no_jar':needs_missing,'tags_cobblemon_ausentes':encoded(sorted(set(missing_tags))),'requer_mods':encoded(required),'incompativel_mods':encoded(doc.get('neededUninstalledMods',[])),
    'condicoes':encoded(s.get('condition',{})),'anticondicoes':encoded(s.get('anticondition',{})),
    'presets':encoded(s.get('presets',[])),'requisitos_presets':encoded(preset_data),
    'multiplicadores':encoded({k:s[k] for k in ['weightMultiplier','weightMultipliers'] if k in s}),
    'restricoes_para_revisao':encoded(flags_for(s)),'id_entrada':s.get('id',''),'arquivo_origem':path
   })
by_species=collections.defaultdict(list)
for row in rows:by_species[row['pokemon']].append(row)
summary=[]
for sp,(data,path) in sorted(species.items(),key=lambda pair:pair[1][0].get('nationalPokedexNumber',99999)):
 all_rows=by_species[sp];ordinary=[r for r in all_rows if r['bucket'] in BUCKETS];core=[r for r in ordinary if not r['mod_externo_explicito'] and not r['requer_tag_cobblemon_sem_definicao_no_jar']]
 common=[r for r in core if r['bucket']=='common'];best=next((b for b in BUCKETS if any(r['bucket']==b for r in core)),None)
 hard={'isSlimeChunk','isPokeSnack','structures','neededNearbyBlocks','neededBaseBlocks','minLureLevel','fishingBait'}
 special=bool(common) and all(any(flag in hard or flag.startswith('preset:') for flag in json.loads(r['restricoes_para_revisao'])) for r in common)
 if not data.get('implemented',False):category='sem_implemented_true'
 elif best:category=BUCKET_PT[best]+('_condicionado' if best=='common' and special else '')
 elif ordinary:category='rotas_dependentes_ou_nao_resolvidas'
 elif all_rows:category='somente_grupo_boss_alpha'
 else:category='sem_rota_neste_pool'
 labels=data.get('labels',[])
 summary.append({'dex':data.get('nationalPokedexNumber',''),'pokemon':sp,'nome':data.get('name',sp),'implemented':data.get('implemented',False),
  'categoria_observada':category,'menor_bucket_core_observado':best or '',
  'buckets_normais':','.join(b for b in BUCKETS if any(r['bucket']==b for r in ordinary)),
  'tem_rota_common_core':bool(common),'common_so_condicionado':special,
  'metodos':','.join(sorted(set(r['metodo'] for r in ordinary))),
  'pesca':any(r['metodo']=='fishing' for r in ordinary),'entradas_individuais':sum(r['tipo_entrada']=='pokemon' for r in ordinary),
  'registros_membro_grupo_normal':sum(r['tipo_entrada']=='pokemon-herd' for r in ordinary),'registros_grupo_boss':sum(r['bucket']=='boss' for r in all_rows),
  'catch_rate_base':data.get('catchRate',45),'egg_groups':','.join(data.get('eggGroups',[])),
  'legendario_mitico_ub':bool(set(labels)&{'legendary','mythical','ultra_beast','ultra-beast'}),
  'labels':','.join(labels),'strings_propriedades_distintas':len(set(r['propriedades'] for r in all_rows)),
  'arquivos_pool':encoded(sorted(set(r['arquivo_origem'] for r in all_rows))),'arquivo_especie':path})
a.out.mkdir(parents=True,exist_ok=True)
def csv_file(name,values):
 with (a.out/name).open('w',encoding='utf-8-sig',newline='') as f:
  w=csv.DictWriter(f,fieldnames=list(values[0]),delimiter=';');w.writeheader();w.writerows(values)
csv_file('pokemon-resumo.csv',summary);csv_file('rotas-spawn.csv',rows);csv_file('rotas-pesca.csv',[r for r in rows if r['metodo']=='fishing'])
metadata={'version':manifest['version'],'license':manifest.get('license'),'jar_sha512':hashlib.sha512(raw).hexdigest(),'jar_sha256':hashlib.sha256(raw).hexdigest(),
 'official_download':'https://cdn.modrinth.com/data/MdwFAVRL/versions/IgUZQKm8/Cobblemon-fabric-1.8.3%2B1.21.1.jar',
 'species_definitions':len(species),'species_implemented_true':sum(r['implemented'] for r in summary),'spawn_pool_files':files,'disabled_pool_files':disabled,
 'parent_entry_types':dict(parent_types),'parent_entry_buckets':dict(parent_buckets),'flattened_species_rows':len(rows),
 'species_with_any_row':sum(bool(by_species[sp]) for sp in species),'species_with_normal_row':sum(any(r['bucket'] in BUCKETS for r in by_species[sp]) for sp in species),
 'rows_requiring_undefined_cobblemon_tag':sum(r['requer_tag_cobblemon_sem_definicao_no_jar'] for r in rows),'undefined_cobblemon_tags':sorted(set(t for r in rows for t in json.loads(r['tags_cobblemon_ausentes']))),
 'categories_observed':dict(collections.Counter(r['categoria_observada'] for r in summary)),
 'default_bucket_weights':{k:config[k] for k in ['worldBuckets','fishingBuckets','activatedHabitatBuckets','pokeSnackBuckets']},
 'empty_spawn_entries':empty,'probabilities_per_species_computed':False,
 'limits':['Peso não é probabilidade final.','Registro de membro de grupo não é tentativa independente.','Core exclui apenas referências explicitamente externas; não simula biomas/tags/eventos ou presets no mundo real.','Categoria observada não é preço aprovado.','Sem rota neste pool não significa impossibilidade de obter por evolução, reprodução, fóssil, recompensa ou addons.']}
(a.out/'metadata.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
checks=[]
checks.append(('1025 definitions',len(species)==1025));checks.append(('905 implemented',metadata['species_implemented_true']==905))
checks.append(('world buckets sum100',abs(sum(config['worldBuckets'].values())-100)<1e-8));checks.append(('fishing buckets sum100',abs(sum(config['fishingBuckets'].values())-100)<1e-8))
for sp,bucket,weight,method in [('magikarp','common',90.0,'fishing'),('gyarados','common',10.0,'fishing'),('feebas','common',57.0,'fishing'),('dratini','common',4500.0,'submerged'),('bulbasaur','ultra-rare',18.0,'grounded')]:
 checks.append((sp+' known route',any(r['bucket']==bucket and r['peso_entrada']==weight and r['metodo']==method and r['tipo_entrada']=='pokemon' for r in by_species[sp])))
checks.append(('Undefined Dratini structure tag flagged',any(r['pokemon']=='dratini' and r['bucket']=='common' and r['requer_tag_cobblemon_sem_definicao_no_jar'] for r in rows)));checks.append(('Mewtwo no row',not by_species['mewtwo']));checks.append(('herd weights separate',any(r['tipo_entrada']=='pokemon-herd' and r['peso_membro']!='' for r in rows)))
checks.append(('Every row known species',all(r['pokemon'] in species for r in rows)))
(a.out/'validation.json').write_text(json.dumps({'passed':all(v for _,v in checks),'checks':[{'name':n,'passed':v} for n,v in checks]},indent=2)+'\n')
assert all(v for _,v in checks)
print(json.dumps({k:metadata[k] for k in ['species_definitions','species_implemented_true','spawn_pool_files','flattened_species_rows','species_with_any_row','species_with_normal_row','categories_observed']},ensure_ascii=False,indent=2))
