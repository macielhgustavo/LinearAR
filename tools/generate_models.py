"""Generate original compressor parts as GLB. Requires numpy. All output is CC0."""
import json,struct,math
from pathlib import Path
import numpy as np
OUT=Path(__file__).resolve().parents[1]/'app/src/main/assets/models'
OUT.mkdir(parents=True,exist_ok=True)
parts={};current=[]
TEAL=[.07,.42,.47,1];DARK=[.055,.09,.12,1];STEEL=[.58,.66,.7,1];ORANGE=[.98,.45,.13,1];GOLD=[.64,.44,.17,1];WHITE=[.88,.91,.9,1];RED=[.88,.12,.16,1]
def mesh(v,f,c):current.append((np.array(v,dtype=float),f,c))
def box(c,s,col):
 v=np.array([[x,y,z] for x in [-1,1] for y in [-1,1] for z in [-1,1]])*np.array(s)/2+np.array(c)
 mesh(v,[[0,2,3,1],[4,5,7,6],[0,1,5,4],[2,6,7,3],[0,4,6,2],[1,3,7,5]],col)
def cyl(c,r,h,col,axis=0,n=48):
 v=[]
 for side in [-1,1]:
  for a in np.linspace(0,2*math.pi,n,endpoint=False):
   p=[r*math.cos(a),r*math.sin(a),side*h/2]
   if axis==0:p=[p[2],p[0],p[1]]
   elif axis==1:p=[p[0],p[2],p[1]]
   v.append(np.array(c)+p)
 mesh(v,[[i,(i+1)%n,(i+1)%n+n,i+n] for i in range(n)]+[list(range(n-1,-1,-1)),list(range(n,2*n))],col)
def ellipsoid(c,s,col):
 n=48;m=16;v=[]
 for j in range(m+1):
  a=math.pi*j/m
  for i in range(n):
   b=2*math.pi*i/n;v.append(np.array(c)+np.array(s)*[math.cos(a),math.sin(a)*math.cos(b),math.sin(a)*math.sin(b)])
 f=[[j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i] for j in range(m) for i in range(n)]
 mesh(v,f,col)
def begin():global current;current=[]
def end(id):parts[id]=current.copy()
begin()
cyl([0,.38,0],.27,.9,TEAL)
for x in [-.45,.45]:ellipsoid([x,.38,0],[.15,.27,.27],TEAL)
for x in [-.32,.32]:
 cyl([x,.38,0],.274,.035,DARK)
 box([x,.07,0],[.15,.14,.45],DARK)
 for z in [-.15,.15]:cyl([x,.015,z],.055,.03,DARK,axis=1)
box([-.1,.68,0],[.9,.06,.38],DARK)
box([-.2,.39,.273],[.3,.095,.007],WHITE)
box([-.2,.39,.281],[.17,.02,.006],TEAL)
end('tank')
begin()
cyl([-.3,.84,0],.13,.37,ORANGE)
for x in np.linspace(-.47,-.14,10):cyl([x,.84,0],.142,.012,DARK)
cyl([-.5,.84,0],.145,.05,DARK)
for a in np.linspace(0,2*math.pi,10,endpoint=False):cyl([-.532,.84+.1*math.cos(a),.1*math.sin(a)],.012,.008,STEEL,axis=0,n=8)
box([-.3,.77,.15],[.14,.12,.09],ORANGE)
cyl([-.065,.84,0],.047,.11,STEEL)
box([.23,.77,0],[.42,.12,.32],STEEL)
for x in [.1,.34]:
 cyl([x,.95,0],.095,.24,STEEL,axis=1)
 for y in np.linspace(.85,1.04,7):cyl([x,y,0],.115,.02,DARK,axis=1)
 box([x,1.09,0],[.23,.06,.22],STEEL)
 for dx in [-.085,.085]:
  for z in [-.075,.075]:cyl([x+dx,1.125,z],.017,.018,DARK,axis=1,n=8)
cyl([.23,.87,.19],.075,.07,DARK,axis=2)
end('motor')
begin()
cyl([.53,.55,0],.026,.24,GOLD,axis=1)
cyl([.65,.62,0],.03,.24,GOLD)
cyl([.77,.62,0],.045,.075,STEEL)
cyl([.64,.67,0],.015,.1,GOLD,axis=1)
box([.64,.735,0],[.18,.025,.04],RED)
for x in [.55,.77]:cyl([x,.62,0],.044,.05,GOLD)
cyl([.77,.62,0],.024,.08,DARK)
end('valve')
begin()
cyl([.4,.69,.13],.019,.16,GOLD,axis=1)
cyl([.4,.81,.19],.083,.055,DARK,axis=2)
cyl([.4,.81,.223],.072,.012,WHITE,axis=2)
for a in np.linspace(-math.pi*.2,math.pi*1.2,10):
 p=[.4+.056*math.cos(a),.81+.056*math.sin(a),.232];cyl(p,.004,.004,DARK,axis=2,n=8)
box([.412,.836,.237],[.007,.062,.005],RED)
cyl([.4,.81,.24],.009,.01,DARK,axis=2)
end('gauge')
begin()
cyl([.1,.38,0],.035,.9,STEEL)
for x in [-.3,.3]:cyl([x,.54,0],.025,.32,STEEL,axis=1)
cyl([.1,.68,0],.025,.42,GOLD)
end('circuit')
def glb(items,path):
 data=bytearray();views=[];acc=[];meshes=[];mats=[]
 def add(a,component,typ):
  while len(data)%4:data.append(0)
  start=len(data);data.extend(a.tobytes());views.append({'buffer':0,'byteOffset':start,'byteLength':a.nbytes})
  entry={'bufferView':len(views)-1,'componentType':component,'count':len(a),'type':typ}
  if typ=='VEC3':entry.update(min=a.min(axis=0).tolist(),max=a.max(axis=0).tolist())
  acc.append(entry);return len(acc)-1
 for v,faces,col in items:
  positions=[];normals=[]
  for f in faces:
   for k in range(1,len(f)-1):
    t=v[[f[0],f[k],f[k+1]]];n=np.cross(t[1]-t[0],t[2]-t[0]);length=np.linalg.norm(n)
    if length<1e-9:continue
    n/=length;positions.extend(t);normals.extend([n]*3)
  pos=add(np.array(positions,dtype='<f4'),5126,'VEC3');normal=add(np.array(normals,dtype='<f4'),5126,'VEC3')
  mats.append({'doubleSided':True,'pbrMetallicRoughness':{'baseColorFactor':col,'metallicFactor':.35 if col in [STEEL,GOLD] else .05,'roughnessFactor':.42}})
  meshes.append({'primitives':[{'attributes':{'POSITION':pos,'NORMAL':normal},'material':len(mats)-1}]})
 g={'asset':{'version':'2.0','generator':'LinearAR original compressor generator','copyright':'CC0 1.0 Universal'},'scene':0,'scenes':[{'nodes':list(range(len(meshes)))}],'nodes':[{'mesh':i} for i in range(len(meshes))],'meshes':meshes,'materials':mats,'bufferViews':views,'accessors':acc,'buffers':[{'byteLength':len(data)}]}
 j=json.dumps(g,separators=(',',':')).encode();j+=b' '*(-len(j)%4);data+=b'\0'*(-len(data)%4)
 path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(j)+len(data))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(data),0x004e4942)+data)
for id,items in parts.items():glb(items,OUT/f'{id}.glb')
glb([item for id,items in parts.items() if id!='circuit' for item in items],OUT/'compressor.glb')
print('Generated six original GLB assets')
