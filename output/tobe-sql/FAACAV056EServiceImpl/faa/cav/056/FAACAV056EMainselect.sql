/* faa/cav/056/FAACAV056EMainselect */ SELECT ROWNUM,
    BBS_SECD /* 게시판구분코드 */,
    FRS_MDM_SQNO /* 포렌식매체순번 */,
    GROUP_NO /* 그룹번호 */,
    RSCH_ASMT_TTL /* 연구과제제목 */,
    REG_DT /* 등록일시 */,
    REG_INST_NM /* 등록기관명 */,
    RGTR_ID /* 등록자아이디 */,
    RGTR_NM /* 등록자명 */,
    ENAIS_PST_INQ_NMTM /* ENAIS게시물조회횟수 */,
    TO_CHAR(BBS_CN) AS BBS_CN /* 게시판내용 */,
    CVLCPT_DOC_ID /* 민원문서아이디 */
FROM TWFAACAV065L WHERE 1 = 1 AND TO_CHAR(REG_DT,'YYYYMMDD') BETWEEN #strSrchFrom# AND #strSrchTo# AND BBS_SECD = 'NO' AND FRS_MDM_SQNO = #FRS_MDM_SQNO# AND (RSCH_ASMT_TTL like '%'||#strSrchNm#||'%' OR BBS_CN like '%'||#strSrchNm#||'%') AND RSCH_ASMT_TTL like '%'||#strSrchNm#||'%' AND BBS_CN like '%'||#strSrchNm#||'%' ORDER BY KEY DESC