/* faa/cav/056/FAACAV056EDetailselect */ SELECT ROWNUM,
    BBS_SECD /* 게시판구분코드 */,
    FRS_MDM_SQNO /* 포렌식매체순번 */,
    GROUP_NO /* 그룹번호 */,
    RSCH_ASMT_TTL /* 연구과제제목 */,
    TO_CHAR(REG_DT,'YYYYMMDD') AS REG_DT /* 등록일시 */,
    RGTR_NM /* 등록자명 */,
    ENAIS_PST_INQ_NMTM /* ENAIS게시물조회횟수 */,
    TO_CHAR(BBS_CN) AS BBS_CN /* 게시판내용 */,
    CVLCPT_DOC_ID /* 민원문서아이디 */
FROM TWFAACAV065L WHERE 1 = 1 AND BBS_SECD = #BBS_SECD# AND FRS_MDM_SQNO = #FRS_MDM_SQNO# ORDER BY FRS_MDM_SQNO DESC