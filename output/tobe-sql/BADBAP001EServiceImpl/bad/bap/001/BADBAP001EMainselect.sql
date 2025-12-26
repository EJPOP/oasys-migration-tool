SELECT D1.LINK_YR /* 연계년도 */,
    D1.ADT_PLAN_DOC_SQNO /* 감사계획문서순번 */,
    D1.RSCH_ASMT_TTL /* 연구과제제목 */,
    D1.ADT_PLAN_DOC_KDCD /* 감사계획문서종류코드 */,
    D1.SRECS_DEPT_CD /* 심사재심의부서코드 */,
    F_DPT_INFO(D1.SRECS_DEPT_CD,'1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    D1.CVLCPT_DOC_ID /* 민원문서아이디 */,
    D1.ADT_PLAN_DOC_TPCD /* 감사계획문서유형코드 */,
    D1.FRST_USER_ID /* 최초사용자아이디 */,
    D1.LAST_USER_ID /* 최종사용자아이디 */,
    D1.LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    D1.LAST_MENU_ID /* 최종메뉴아이디 */,
    TO_CHAR(D1.FRST_REG_DT,'YYYYMMDD') AS FRST_REG_DT /* 최초등록일시 */,
    TO_CHAR(D1.LAST_MDFCN_DT,'YYYYMMDD') AS FRST_REG_DT /* 최초등록일시 */,
    D1.LINK_YR /* 연계년도 */,
    D1.ADT_PLAN_DOC_SQNO /* 감사계획문서순번 */
FROM TB_BADBAP002M D1 /* 감사계획문서기본 */ WHERE 1 = 1 AND D1.LINK_YR = #LINK_YR# AND D1.ADT_PLAN_DOC_KDCD IN ('2','3','4') AND D1.ADT_PLAN_DOC_KDCD = #ADT_PLAN_DOC_KDCD# AND D1.SRECS_DEPT_CD IN (SELECT SRECS_DEPT_CD /* 심사재심의부서코드 */
FROM TABLE(F_AUTH_DEPT(#strCnb#, #strDptAuth#, '', #strDptCd#))) AND D1.SRECS_DEPT_CD = #SRECS_DEPT_CD# ORDER BY D1.LINK_YR DESC, D1.ADT_PLAN_DOC_SQNO DESC