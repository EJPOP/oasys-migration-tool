/*cspsae902 CSPSAE902EselectMain*/ SELECT A.INST_CD /* 기관코드 */,
    F_ORG_INFO(A.INST_CD,'1') AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    A.RSCH_ASMT_TTL /* 연구과제제목 */,
    A.BBS_CN /* 게시판내용 */,
    A.USER_HSTRY_SECD /* 사용자이력구분코드 */,
    A.GRNDS_EXMN_AEX_SQNO /* 현장조사수용비순번 */,
    A.CVLCPT_DOC_ID /* 민원문서아이디 */,
    A.APL_WAY_CD
FROM TBCSPSAE902M A WHERE A.GRNDS_EXMN_AEX_SQNO = #GRNDS_EXMN_AEX_SQNO#