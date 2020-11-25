package com.vitimage.common;

public class SentenceOfTheDay {
	public static String getSentence() {
		String[]sentences=VitimageUtils.readStringFromFile("src/main/resources/Zen_Quotes.txt").split("\n");
		int n=sentences.length-1;
		int val=new Timer().getAbsoluteDay();
		int chosen=1+val%n;
		return sentences[chosen];
	}
}
