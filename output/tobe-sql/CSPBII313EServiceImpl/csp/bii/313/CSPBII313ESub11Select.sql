SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.STTM_YE /* 신고연도 */,
    A.FRTN_AMT /* 재산액 */,
    A.INCR_AMT /* 증가액 */,
    A.CVLCPT_DOC_ID /* 민원문서아이디 */,
    A.ADT_PLAN_DOC_TPCD /* 감사계획문서유형코드 */
FROM TBCSPBII360L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC