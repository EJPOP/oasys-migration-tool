SELECT T1.NDTY_DT /* 서류등록일자 */,
    T1.NDTY_DOC_SEQ /* 서류접수순번 */,
    T1.NDTY_TIT_NM /* 접수서류제목 */,
    T1.NDTY_CNB /* 등록자전산번호 */,
    T1.NDTY_NAM /* 등록자명 */,
    T1.NDTY_DOC_ID /* 당직서류ID */,
    T1.FRST_USER_ID /* 최초사용자아이디 */,
    T1.LAST_USER_ID /* 최종사용자아이디 */,
    T1.LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    T1.LAST_MENU_ID /* 최종메뉴아이디 */,
    T1.FRST_REG_DT /* 최초등록일시 */,
    T1.LAST_MDFCN_DT /* 최종수정일시 */
FROM TBCSPOEP006M T1 WHERE 1=1 AND (TO_CHAR(TO_DATE(T1.NDTY_DT, 'YYYYMMDD'), 'YYYYMMDD') BETWEEN #strNDTY_DTS# AND #strNDTY_DTE#) AND T1.NDTY_TIT_NM LIKE '%'||#strNDTY_TIT_NM#||'%' AND T1.NDTY_NAM LIKE '%'||#strNDTY_NAM#||'%' ORDER BY T1.NDTY_DOC_SEQ DESC