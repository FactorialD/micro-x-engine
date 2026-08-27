"""Tkinter desktop UI. Parsing and validation live in headless-testable modules."""
from pathlib import Path
import tkinter as tk
from tkinter import filedialog,messagebox,ttk
from .io import Project,ProjectError
from .images import inspect_png,replace_atlas
from .obj import validate_obj,replace_obj
from .level import parse_level_text,serialize_level
from .data import parse_data,serialize_data,load_tables,validate_tables
class Editor(tk.Tk):
    def __init__(self):
        super().__init__(); self.title("Micro X Editor"); self.geometry("1000x650")
        self.project=None; self.current=None; self.dirty=False; self.photo=None
        bar=ttk.Frame(self); bar.pack(fill="x")
        ttk.Button(bar,text="Open project",command=self.open_project).pack(side="left")
        ttk.Button(bar,text="Save",command=self.save).pack(side="left")
        ttk.Button(bar,text="Replace resource",command=self.replace).pack(side="left")
        self.status=ttk.Label(bar,text="Choose a project"); self.status.pack(side="left",padx=12)
        pane=ttk.Panedwindow(self,orient="horizontal"); pane.pack(fill="both",expand=True)
        self.tree=ttk.Treeview(pane,show="tree"); self.tree.bind("<<TreeviewSelect>>",self.select); pane.add(self.tree,weight=1)
        right=ttk.Frame(pane); pane.add(right,weight=4)
        self.info=ttk.Label(right,text="",justify="left"); self.info.pack(fill="x")
        self.text=tk.Text(right,undo=True); self.text.pack(fill="both",expand=True); self.text.bind("<<Modified>>",self.modified)
        self.protocol("WM_DELETE_WINDOW",self.close)
    def error(self,e): messagebox.showerror("Micro X Editor",str(e))
    def open_project(self):
        try:
            selected=filedialog.askdirectory()
            if not selected:return
            self.project=Project(selected); self.current=None; self.tree.delete(*self.tree.get_children())
            root=self.project.path("assets-src",existing=True)
            nodes={root:self.tree.insert("","end",text="assets-src",open=True,values=(str(root),))}
            for p in sorted(root.rglob("*")):
                parent=nodes.get(p.parent,nodes[root]); node=self.tree.insert(parent,"end",text=p.name,values=(str(p),)); nodes[p]=node
            self.status.config(text=str(self.project.root))
        except Exception as e:self.error(e)
    def select(self,event=None):
        choice=self.tree.selection()
        if not choice:return
        p=Path(self.tree.item(choice[0],"values")[0])
        if not p.is_file():return
        if self.dirty and not messagebox.askyesno("Unsaved changes","Discard unsaved changes?"):return
        try:
            if p.suffix in (".mesh",".lvl",".tex",".dat"): raise ValueError("Generated runtime files are read-only")
            self.current=p; self.text.config(state="normal"); self.text.delete("1.0","end"); info=p.name
            if p.suffix==".png":
                data=inspect_png(p); self.photo=tk.PhotoImage(file=str(p)); self.info.config(image=self.photo,compound="bottom",text=f"{data['width']}×{data['height']}, {data['colors']} colors, {data['footprint']} bytes")
                self.text.config(state="disabled")
            else:
                content=p.read_text(encoding="utf-8")
                if p.suffix==".obj": info += " — "+str(validate_obj(p))
                elif p.suffix==".level": parse_level_text(content); info += " — MXL2 table/2D source (one record per row)"
                elif p.suffix==".txt" and "assets-src/data" in p.as_posix(): parse_data(content); info += " — id | key | description | meta"
                self.info.config(image="",text=info); self.text.insert("1.0",content); self.text.edit_modified(False)
            self.dirty=False; self.mark()
        except Exception as e:self.error(e)
    def modified(self,event=None):
        if self.text.edit_modified(): self.dirty=True; self.mark(); self.text.edit_modified(False)
    def mark(self): self.title("Micro X Editor"+(" *" if self.dirty else ""))
    def save(self):
        if not self.current or not self.dirty:return
        try:
            text=self.text.get("1.0","end-1c")
            if self.current.suffix==".level": text=serialize_level(parse_level_text(text))
            elif self.current.suffix==".txt":
                table=parse_data(text); tables=load_tables(self.project); tables[self.current.stem]=table; validate_tables(tables); text=serialize_data(table)
            elif self.current.suffix==".obj": raise ValueError("Use Replace resource for OBJ files")
            else: raise ValueError("This file type is read-only")
            self.project.atomic_write(self.current,text); self.dirty=False; self.mark(); self.status.config(text="Saved atomically")
        except Exception as e:self.error(e)
    def replace(self):
        if not self.current:return
        source=filedialog.askopenfilename()
        if not source:return
        try:
            if self.current.suffix==".png": result=replace_atlas(self.project,source,self.current)
            elif self.current.suffix==".obj": result=replace_obj(self.project,source,self.current)
            else: raise ValueError("Only source PNG and OBJ resources can be replaced")
            self.status.config(text="Replaced: "+str(result)); self.select()
        except Exception as e:self.error(e)
    def close(self):
        if not self.dirty or messagebox.askyesno("Unsaved changes","Exit without saving?"):self.destroy()
def main(): Editor().mainloop()
if __name__=="__main__":main()
