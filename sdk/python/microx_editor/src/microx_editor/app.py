"""Structured Tkinter editor; parsers remain independently testable."""
from pathlib import Path
import tkinter as tk
from tkinter import filedialog,messagebox,ttk
from .io import Project
from .images import inspect_png,replace_atlas
from .obj import validate_obj,replace_obj
from .level import KINDS,ARITY,Line,parse_level_text,serialize_level
from .data import (DataLine,Row,ITEM_SCHEMAS,COMMON_ITEM_FIELDS,TABLE_METADATA,
                   parse_data,parse_metadata,serialize_metadata,metadata_fields,
                   serialize_data,load_tables,validate_tables)

def resolve_unsaved(dirty, answer, save):
 if not dirty:return True
 if answer is None:return False
 if answer:save();return True
 return True

class Editor(tk.Tk):
 def __init__(self):
  super().__init__();self.title('Micro X Editor');self.geometry('1100x700')
  self.project=None;self.current=None;self.model=None;self.dirty=False;self.photo=None;self.loading=False
  bar=ttk.Frame(self);bar.pack(fill='x')
  for label,command in [('Open project',self.open_project),('Save',self.save),('Save All',self.save),('Validate Project',self.validate_project),('Replace resource',self.replace)]:ttk.Button(bar,text=label,command=command).pack(side='left')
  self.status=ttk.Label(bar,text='Choose a project');self.status.pack(side='left',padx=12)
  pane=ttk.Panedwindow(self,orient='horizontal');pane.pack(fill='both',expand=True)
  self.tree=ttk.Treeview(pane,show='tree');self.tree.bind('<<TreeviewSelect>>',self.select);pane.add(self.tree,weight=1)
  right=ttk.Frame(pane);pane.add(right,weight=4);self.info=ttk.Label(right);self.info.pack(fill='x')
  tools=ttk.Frame(right);tools.pack(fill='x')
  for label,command in [('Add',self.add),('Delete',self.delete),('Move Up',lambda:self.move(-1)),('Move Down',lambda:self.move(1))]:ttk.Button(tools,text=label,command=command).pack(side='left')
  self.kind=tk.StringVar();self.filter=ttk.Combobox(tools,textvariable=self.kind,state='readonly');self.filter.bind('<<ComboboxSelected>>',lambda e:self.render());self.filter.pack(side='left')
  self.grid=ttk.Treeview(right,show='headings',selectmode='browse');self.grid.bind('<<TreeviewSelect>>',self.load_form);self.grid.pack(fill='both',expand=True)
  self.form=ttk.Frame(right);self.form.pack(fill='x');self.entries=[];self.form_fields=[]
  self.raw=tk.Text(right,height=5,undo=True);self.raw.pack(fill='x');self.raw.bind('<<Modified>>',self.raw_modified)
  ttk.Label(right,text='Advanced: raw source (read-only mirror; structured forms are authoritative)').pack(anchor='w');self.raw.config(state='disabled')
  self.protocol('WM_DELETE_WINDOW',self.close)
 def error(self,e):messagebox.showerror('Micro X Editor',str(e))
 def guard(self):
  if not self.dirty:return True
  answer=messagebox.askyesnocancel('Unsaved changes','Save changes before continuing?')
  if answer is None:return False
  if answer:self.save();return not self.dirty
  return True
 def open_project(self):
  selected=filedialog.askdirectory()
  if not selected or not self.guard():return
  try:
   project=Project(selected);load_tables(project) # full converter-contract validation
   root=project.path('res',existing=True);self.project=project;self.current=None;self.model=None
   self.tree.delete(*self.tree.get_children());nodes={root:self.tree.insert('','end',text='res',open=True,values=(str(root),))}
   for p in sorted(root.rglob('*')):nodes[p]=self.tree.insert(nodes.get(p.parent,nodes[root]),'end',text=p.name,values=(str(p),))
   self.status.config(text=str(project.root));self.dirty=False;self.mark()
  except Exception as e:self.error(e)
 def select(self,event=None):
  choice=self.tree.selection()
  if not choice:return
  p=Path(self.tree.item(choice[0],'values')[0])
  if not p.is_file():return
  if not self.guard():return
  old=(self.current,self.model)
  try:
   if p.suffix in ('.mesh','.lvl','.tex','.dat') or 'build/generated-resources' in p.as_posix():raise ValueError('Generated runtime files are read-only')
   content=p.read_text(encoding='utf-8') if p.suffix!='.png' else ''
   if p.suffix=='.level':model=parse_level_text(content);mode='level'
   elif p.suffix=='.txt' and '/res/data/' in p.as_posix():model=parse_data(content,str(p));mode='data'
   elif p.suffix=='.png':model=inspect_png(p);mode='image';self.photo=tk.PhotoImage(file=str(p))
   elif p.suffix=='.obj':model=validate_obj(p);mode='other'
   else:raise ValueError('This source type is read-only')
   self.current=p;self.model=model;self.mode=mode;self.dirty=False;self.render();self.mark()
  except Exception as e:self.current,self.model=old;self.error(e)
 def render(self):
  self.loading=True;self.grid.delete(*self.grid.get_children());self._clear_form()
  if not self.current:self.loading=False;return
  if self.mode=='level':
   self.filter.config(values=KINDS);kind=self.kind.get() if self.kind.get() in KINDS else KINDS[0];self.kind.set(kind)
   cols=tuple(f'field {i+1}' for i in range(ARITY[kind]));self._columns(cols)
   for line in self.model.records(kind):self.grid.insert('','end',values=line.values)
   raw=serialize_level(self.model)
  elif self.mode=='data':
   self.filter.config(values=());self.kind.set('');self._columns(('id','key','description','metadata'))
   for line in self.model.row_lines():self.grid.insert('','end',values=(line.row.id,line.row.key,line.row.description,line.row.meta))
   raw=serialize_data(self.model)
  else:
   self.filter.config(values=());self.kind.set('');self._columns(('property','value'))
   for k,v in self.model.items():self.grid.insert('','end',values=(k,v));raw=''
  self.raw.config(state='normal');self.raw.delete('1.0','end');self.raw.insert('1.0',raw);self.raw.edit_modified(False);self.raw.config(state='disabled');self.loading=False
  self.info.config(text=f'{self.current.name} — structured {self.mode} editor')
 def _columns(self,cols):
  self.grid.config(columns=cols)
  for c in cols:self.grid.heading(c,text=c);self.grid.column(c,width=120)
 def _clear_form(self):
  for child in self.form.winfo_children():child.destroy()
  self.entries=[];self.form_fields=[]
 def load_form(self,event=None):
  self._clear_form();selected=self.grid.selection()
  if not selected or self.mode not in ('level','data'):return
  values=list(self.grid.item(selected[0],'values'));labels=list(self.grid['columns'])
  if self.mode=='data':
   table=self.current.stem
   metadata=parse_metadata(values.pop(),f'{self.current}: record {self.grid.index(selected[0])+1}: metadata')
   labels=list(self.grid['columns'][:-1])+list(metadata_fields(table,metadata));values+= [metadata.get(k,'') for k in labels[3:]]
  self.form_fields=labels
  for i,(label,value) in enumerate(zip(labels,values)):
   ttk.Label(self.form,text=label).grid(row=i//4*2,column=i%4,sticky='w');var=tk.StringVar(value=value);entry=ttk.Entry(self.form,textvariable=var);entry.grid(row=i//4*2+1,column=i%4,sticky='ew');entry.bind('<FocusOut>',self.apply_form);self.entries.append(var)
 def apply_form(self,event=None):
  selected=self.grid.selection()
  if not selected:return
  index=self.grid.index(selected[0]);values=[v.get() for v in self.entries]
  try:
   if self.mode=='level':self.model.records(self.kind.get())[index].values=values
   else:
    line=self.model.row_lines()[index];fields=self.form_fields[3:]
    metadata=serialize_metadata(dict(zip(fields,values[3:])),fields)
    line.row=Row(int(values[0]),values[1],values[2],metadata);line.had_meta=bool(metadata) or line.had_meta
   self.changed();self.render()
  except Exception as e:self.error(f'{self.current}: record {index+1}: {e}')
 def changed(self):self.dirty=True;self.mark()
 def add(self):
  if self.mode=='level':
   kind=self.kind.get();insert=len(self.model.lines)
   for i,line in enumerate(self.model.lines):
    if line.kind in KINDS and KINDS.index(line.kind)>KINDS.index(kind):insert=i;break
   self.model.lines.insert(insert,Line('',kind,['0']*ARITY[kind]))
  elif self.mode=='data':
   rows=self.model.rows();ident=max((row.id for row in rows),default=0)+1
   self.model.lines.append(DataLine('',Row(ident,f'new_key_{ident}','', ''),False,len(self.model.lines)+1))
  else:return
  self.changed();self.render()
 def delete(self):
  selected=self.grid.selection()
  if not selected:return
  line=(self.model.records(self.kind.get()) if self.mode=='level' else self.model.row_lines())[self.grid.index(selected[0])];self.model.lines.remove(line);self.changed();self.render()
 def move(self,delta):
  selected=self.grid.selection()
  if not selected:return
  rows=self.model.records(self.kind.get()) if self.mode=='level' else self.model.row_lines();line=rows[self.grid.index(selected[0])];target=self.grid.index(selected[0])+delta
  if not 0<=target<len(rows):return
  self.model.lines.remove(line);anchor=rows[target];self.model.lines.insert(self.model.lines.index(anchor)+(delta>0),line);self.changed();self.render()
 def validate_project(self):
  try:validate_tables(load_tables(self.project));self.status.config(text='Project is valid')
  except Exception as e:self.error(e)
 def save(self):
  if not self.current or not self.dirty:return
  try:
   if self.mode=='level':text=serialize_level(self.model)
   elif self.mode=='data':
    tables=load_tables(self.project);tables[self.current.stem]=self.model;validate_tables(tables);text=serialize_data(self.model)
   else:raise ValueError('Use Replace resource')
   self.project.atomic_write(self.current,text);self.dirty=False;self.mark();self.status.config(text='Saved atomically')
  except Exception as e:self.error(e)
 def replace(self):
  if not self.current or not self.guard():return
  source=filedialog.askopenfilename()
  if not source:return
  try:
   result=replace_atlas(self.project,source,self.current) if self.current.suffix=='.png' else replace_obj(self.project,source,self.current)
   self.status.config(text='Replaced: '+str(result));self.select()
  except Exception as e:self.error(e)
 def raw_modified(self,event=None):self.raw.edit_modified(False)
 def mark(self):self.title('Micro X Editor'+(' *' if self.dirty else ''))
 def close(self):
  if self.guard():self.destroy()
def main():Editor().mainloop()
if __name__=='__main__':main()
