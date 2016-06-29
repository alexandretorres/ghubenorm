package gitget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * Represents directories as a tree
 * @author torres
 *
 */
public class Dir {
	//public static final JavaDir root = new JavaDir("");
	String name;
	Dir parent;
	List<Dir> children;
	public static Dir newRoot() {
		return new Dir("");
	}
	private Dir(String name) {
		this.name = name;
		this.children = new ArrayList<Dir>();
	}
	public Dir(String name,Dir parent) {
		this.name = name;
		this.parent = parent;
		parent.addChild(this);
	}
	/**
	 * add child, merging grand-children
	 * @param dir
	 */
	public Dir addChild(Dir dir) {
		if (children==null)
			children=new ArrayList<Dir>();
		Dir ret = get(dir.name);
		if (ret==null) {
			children.add(dir);
			dir.parent=this;
			ret = dir;
		} else {
			if (dir.children!=null && !dir.children.isEmpty()) {
				for (Dir c:dir.children) {
					ret.addChild(c);
				}
				dir.parent=null;
				dir.children=null;
			}			
		}		
		return ret;
	}
	public Dir get(String pname) {
		if (children==null)
			return null;
		return children.stream().filter(c->c.name.equals(pname)).findFirst().orElse(null);
	}
	public Dir rebase(Dir root,Dir newRoot) {
		if (parent==null)
			throw new RuntimeException("Could not rebase the root");
		String basePath = parent.getPath();
		remove();
		return newRoot.addAt(basePath, this);		
	}
	/**
	 * Remove this path, and recursively eliminates all parents without children
	 * @param dir
	 */
	public void remove() {		
		if (parent==null)
			return;
		parent.children.remove(this);		
		if (parent.children.isEmpty()) {
			parent.remove();
		}
		this.parent=null;
	}
	/**
	 * Add a dir at an specific path, creating the parent folders if necessary
	 * @param path
	 * @param dir
	 */
	public Dir addAt(String path,Dir dir) {
		Dir base = register(path);
		return base.addChild(dir);
	}
	/**
	 * Creates a JavaDir with a path relative to current branch. 
	 * @param path
	 */
	public Dir register(String path) {
		String way[] = path.split("/");
		Dir cur = this;
		for (String step:way) {
			if (step.equals(""))
				continue;
			Dir child = cur.get(step);
			if (child==null) {
				child = new Dir(step, cur);
			}
			cur=child;
		}
		return cur;
	}
	/**
	 * Find a JavaDir
	 * @param path path relative to current branch
	 */
	public Dir find(String path) {		
		String way[] = path.split("/");
		Dir cur = this;
		for (String step:way) {
			if (step.equals(""))
				continue;
			Dir child = cur.get(step);
			if (child==null) {
				return null;
			}
			cur=child;
		}
		return cur;
	}
	void removeTestFolders() {
		if (this.children==null)
			return;
		for (Iterator<Dir> it=this.children.iterator();it.hasNext();) {
			Dir d = it.next();
			if (d.name.equalsIgnoreCase("test") || d.name.equalsIgnoreCase("tests")) {
				it.remove();
			}
		}
	}
	/**
	 * Return this if it has more than one children or find first descendant with more than one children
	 * @return 
	 */
	Dir getSourceRootCandidate() {
		Dir cur = this;		
	//	String path="";
		while(cur.children.size()==1) {
			Dir child = cur.children.get(0);
		//	path+=cur.name;
			if (child.children==null)
				return cur;
			else
				cur=child;
		}
		return cur;
	}
	public void print() {
		Dir cur = this;
		String path="";
		/*
		while(cur.children.size()==1) {
			cur = cur.children.get(0);
			path+=cur.name;
			if (cur.children==null)
				return;
		}
		for (Dir c:cur.children) {
			System.out.println(path+"/"+c.name);
		}
		System.out.println("-------------------------------------");*/
		for (Dir c:cur.children) {
			c.printChildren(path);
		}
	}
	void printChildren(String path) {
		path +="/"+name;
		System.out.println(path);
		if (children!=null)
			for (Dir c:children) {
				c.printChildren(path);
			}
	}
	public Dir getFirstLeaf() {	
		Dir ret = this;
		while (ret.children!=null && !ret.children.isEmpty()) {
			ret=ret.children.get(0);
		}
		return ret;
		
	}
	public String getPath() {
		if (parent!=null) 
			return parent.getPath()+"/"+name;
		else
			return name;	
	}
	@Override
	public String toString() {
		try {
			return "JavaDir [" + getPath() + "]";
		} catch (Exception ex) {
			return "JavaDir "+name+" Exception:"+ex.getMessage();
		}
	}
	public List<Dir> toLeafList() {
		ArrayList<Dir> all = new ArrayList<Dir>();
		addToList(all,true);
		return all;
		
	}
	public List<Dir> toAllList() {
		ArrayList<Dir> all = new ArrayList<Dir>();
		addToList(all,false);
		return all;
		
	}
	
	private void addToList(List<Dir> list,boolean leafOnly) {
		if (this.children==null || this.children.isEmpty()) {
			list.add(this);
			return;
		}
		if (!leafOnly)
			list.add(this);
		for (Dir child:children) {
			child.addToList(list,leafOnly);
		}
	}
	
}