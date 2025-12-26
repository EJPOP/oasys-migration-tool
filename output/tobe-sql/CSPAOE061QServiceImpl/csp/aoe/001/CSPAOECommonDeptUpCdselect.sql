SELECT /* CSPAOECommonDeptUpCdselect */ F_DPT_INFO(#N/A, '1') AS DEPT_NM /* 부서명 */,
    #N/A AS #N/A /* 부서코드1 */
FROM TBCSPAOE035M WHERE 1=1 AND TSK_DVSN_CD IN ('10000','10001') /*150708_kmh추가_감사지원,감사외*/ GROUP BY DPT_CD_1