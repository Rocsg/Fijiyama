package com.vitimage;

import java.util.ArrayList;

public class RegistrationPipeline {
	public int referenceModality=0;
	public int referenceTime=0;
	public int refTime=0;
	public int refMod=0;
	public int movTime=0;
	public int movMod=1;
	private int step=0;
	private int nTimes=0;
	private int nMods=0;
	private RegistrationAction currentRegAction;
	int estimatedTime=0;
	int estimatedGlobalTime=0;
	ArrayList<ItkTransform> trActions;
	ArrayList<RegistrationAction>regActions;

	ItkRegistrationManager itkManager;
	BlockMatchingRegistration bmRegistration;

	
	public RegistrationPipeline() {
		regActions=new ArrayList<RegistrationAction>();
		trActions=new ArrayList<ItkTransform>();
	}

}
