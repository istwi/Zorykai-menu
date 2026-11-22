"""Conservative species-family acquisition baseline; no claim of universal spawn percentages."""
from pathlib import Path
import csv,json,zipfile,collections,argparse
root=Path(__file__).resolve().parents[1]
rows=list(csv.DictReader((root/'docs/pokemon-sales-research/data/pokemon-resumo.csv').open(encoding='utf-8-sig'),delimiter=';'))
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--cobblemon-jar',type=Path,required=True);jar=parser.parse_args().cobblemon_jar
parents=collections.defaultdict(set);prices={};rank={'common':0,'uncommon':1,'rare':2,'ultra-rare':3}
with zipfile.ZipFile(jar) as z:
 for name in z.namelist():
  if '/species/' not in name or not name.endswith('.json'):continue
  d=json.loads(z.read(name));base=Path(name).stem
  for evolution in d.get('evolutions',[]):
   target=evolution.get('result','').split()[0].split(':')[-1]
   if target and target!=base:parents[target].add(base)
for r in rows:
 k=r['pokemon'];bucket=r['menor_bucket_core_observado'];prices[k]={'bucket':bucket if bucket in rank else '', 'fishing':r['pesca']=='True','parents':sorted(parents[k]),'category':r['categoria_observada']}
dst=root/'src/main/resources/defaults/pokemon/species-profiles.json';dst.parent.mkdir(parents=True,exist_ok=True);dst.write_text(json.dumps(prices,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
print('Generated',len(prices),'species; parent edges',sum(len(v['parents']) for v in prices.values()))
