package com.newrelic.as400;

public class SSTS0400_PoolEntry {
	SSTS0400_PoolEntry() {
		
	}

	// SSTS0400 Pool Information
	public int m_ssts0400_systemPool;                       // offset: 0
	public int m_ssts0400_poolSize;                         // offset: 4
	public int m_ssts0400_reservedSize;                     // offset: 8
	public int m_ssts0400_maximumActiveThreads;             // offset: 12
	public float m_ssts0400_databaseFaults;                 // offset: 16
	public float m_ssts0400_databasePages;                  // offset: 20
	public float m_ssts0400_nondatabaseFaults;              // offset: 24
	public float m_ssts0400_nondatabasePages;               // offset: 28
	public float m_ssts0400_activeToWait;                   // offset: 32
	public float m_ssts0400_waitToIneligible;               // offset: 36
	public int m_ssts0400_activeToIneligible;               // offset: 40
	public String m_ssts0400_poolName;                      // offset: 44
	public String m_ssts0400_subsystemName;                 // offset: 54
	public String m_ssts0400_subsystemLibraryName;          // offset: 64
	public String m_ssts0400_pagingOption;                  // offset: 74
	public int m_ssts0400_definedSize;                      // offset: 84
	public int m_ssts0400_currentThreads;                   // offset: 88
	public int m_ssts0400_currentIneligibleThreads;         // offset: 92
	public int m_ssts0400_tuningPriority;                   // offset: 96
	public int m_ssts0400_tuningMinimumPoolSizePct;         // offset: 100
	public int m_ssts0400_tuningMaximumPoolSizePct;         // offset: 104
	public int m_ssts0400_tuningMinimumFaults;              // offset: 108
	public float m_ssts0400_tuningPerThreadFaults;          // offset: 112
	public float m_ssts0400_tuningMaximumFaults;            // offset: 116
	public String m_ssts0400_description;                   // offset: 120
	public String m_ssts0400_status;                        // offset: 170
	public int m_ssts0400_tuningMinimumActivityLevel;       // offset: 172
	public int m_ssts0400_tuningMaximumActivityLevel;       // offset: 176
	public Double m_ssts0400_poolSize_long;                 // offset: 180
	public Double m_ssts0400_definedSize_long;              // offset: 188
}
