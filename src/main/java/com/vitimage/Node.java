package com.vitimage;
public class Node {
	public int val;
	public Node previous=null;
	public Node next=null;
	public Node(int val) {
		this.val=val;
	}
	public Node(int val,Node previous) {
		this.val=val;
		this.previous=previous;
	}
	public void setNext(Node next) {
		this.next=next;
	}
	public void setPrevious(Node previous) {
		this.previous=previous;
	}
	public Node getPrevious() {
		return previous;
	}
	public Node getNext() {
		return next;
	}
}
