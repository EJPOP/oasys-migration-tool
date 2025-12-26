/*cspsae903 CSPSAE903EselectMain*/ SELECT A.INST_CD /* 기관코드 */,
    F_ORG_INFO(A.INST_CD,'1') AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    A.RSCH_ASMT_TTL /* 연구과제제목 */,
    A.EDU_RCMD_YN,
    A.BBS_CN /* 게시판내용 */,
    A.GRNDS_EXMN_AEX_SQNO /* 현장조사수용비순번 */,
    A.CVLCPT_DOC_ID /* 민원문서아이디 */,
    A.CRS_NM /* 과정명 */
FROM TBCSPSAE903M A WHERE A.GRNDS_EXMN_AEX_SQNO = #GRNDS_EXMN_AEX_SQNO#