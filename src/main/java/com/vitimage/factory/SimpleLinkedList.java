package com.vitimage.factory;

public class SimpleLinkedList {
	private int count=0;
	private Node first=null;
	private Node last=null;
	String title;
	public SimpleLinkedList(String title) {
		this.title=title;
	}

	public Node getFirst() {return first;}
	
	public void add(Node node) {
		if (first==null) {first=node;last=node;count++;}
		else {
			last.next=node;
			node.previous=last;
			last=node;
			count++;
		}
	}

	public void remove(Node node) {
		if (count==0) {System.out.println("No removal possible, void list");System.exit(0);}
		if (count==1) {first=null;count--;}
		else {
			if(node==first) {first=node.next;first.previous=null;count--;}
			else if(node==last) {last=node.previous;last.next=null;count--;}
			else {node.previous.next=node.next; node.next.previous=node.previous;count--;}
		}
	}

	public void pop() {
		remove(first);
	}
	
	public int getCount() {
		return count;
	}


	
	
	
	
	
	public static void main(String[]args) {
		System.out.println("Here1");
		SimpleLinkedList simp=new SimpleLinkedList("My linked list");
		System.out.println("Here2");
		System.out.println(simp);
		System.out.println("Here3");
		Node n10=new Node(10);
		System.out.println("Here4");
		simp.add(n10);
		System.out.println("Here5");
		System.out.println(simp);
		simp.add(new Node(20));
		System.out.println("Here6");
		System.out.println(simp);
		Node n30=new Node(30);
		simp.add(n30);
		System.out.println("Here7");
		System.out.println(simp);
		simp.add(new Node(40));
		System.out.println(simp);
		System.out.println("Here8");
		simp.add(new Node(50));
		System.out.println("Here9");
		System.out.println(simp);
		Node n60=new Node(60);
		simp.add(n60);
		System.out.println("Here10");
		System.out.println(simp);
		simp.remove(n60);
		System.out.println("Here11");
		System.out.println(simp);
		simp.remove(n30);
		System.out.println("Here12");
		System.out.println(simp);
		simp.remove(n10);
		System.out.println("Here13");
		System.out.println(simp);
		simp.pop();
		System.out.println(simp);
		simp.pop();
		System.out.println(simp);
		simp.pop();
		System.out.println(simp);
		simp.pop();
		System.out.println(simp);
		
	}
	
	
	public String toString() {
		String s="SimpleLinkedList "+title+" of size "+count+" .  (First)  ";
		if(count==0)s+=" no nodes ";
		else {
			Node iterNode=first;
			while(iterNode!=null) {
				s+=" --> Node |"+iterNode.val+"|";
				iterNode=iterNode.next;
			}
			s+=" (Last) ";
		}
		return s;
	}
	
	

}
