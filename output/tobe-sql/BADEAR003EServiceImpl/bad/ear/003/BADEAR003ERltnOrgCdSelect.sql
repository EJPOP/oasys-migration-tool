SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    SRECS_REL_INST_CD /* 심사재심의관계기관코드 */,
    REL_INST_CD /* 관계기관코드 */,
    FRST_USER_ID /* 최초사용자아이디 */,
    LAST_USER_ID /* 최종사용자아이디 */,
    LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    LAST_MENU_ID /* 최종메뉴아이디 */
FROM ( SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    SRECS_REL_INST_CD /* 심사재심의관계기관코드 */,
    REL_INST_CD /* 관계기관코드 */,
    FRST_USER_ID /* 최초사용자아이디 */,
    LAST_USER_ID /* 최종사용자아이디 */,
    LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    LAST_MENU_ID /* 최종메뉴아이디 */
FROM TBBADEAR006M WHERE CVLCPT_ADT_YR = #CVLCPT_ADT_YR# AND ADT_NO = #ADT_NO# AND DSPS_RQT_SQNO = #DSPS_RQT_SQNO# ORDER BY FRST_REG_DT, SRECS_REL_INST_CD ) WHERE ROWNUM = 1 /* 1차기관은 1개만 입력 */