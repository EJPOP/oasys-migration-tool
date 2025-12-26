SELECT FRS_MDM_SQNO /* 포렌식매체순번 */,
    MTRL_DVSN_CD,
    F_CODE_NM('1000854',MTRL_DVSN_CD) AS F_CODE_NM,
    INST_CD /* 기관코드 */,
    F_ORG_INFO(INST_CD,'1') AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    RSCH_ASMT_TTL /* 연구과제제목 */,
    DTL_TXT,
    CVLCPT_DOC_ID /* 민원문서아이디 */,
    OTSD_EXPRT_RGTR_ENO /* 외부전문가등록자직원전산번호 */,
    F_USR_INFO(OTSD_EXPRT_RGTR_ENO,'2') AS RGTR_NM /* 등록자명 */,
    TO_CHAR(FRST_REG_DT, 'YYYY-MM-DD') AS TO_CHAR
FROM TBFAAAEA043M WHERE MTRL_DVSN_CD = #strMtrlDvsnCd# AND INST_CD = #INST_CD# AND RSCH_ASMT_TTL LIKE '%'||#strTitNm#||'%'