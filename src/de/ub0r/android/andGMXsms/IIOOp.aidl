package de.ub0r.android.andGMXsms;

interface IIOOp {
	//void updateFreeCount(int connector);
	void sendMessage(int connector, in String[] params);
	String getFailedMessage(int id, out String[] params);
}
