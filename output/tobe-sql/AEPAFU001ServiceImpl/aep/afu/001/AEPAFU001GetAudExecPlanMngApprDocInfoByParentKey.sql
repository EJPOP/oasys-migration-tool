SELECT A.DEPT_CD AS DEPT_CD /* 부서코드 */,
    '' AS ADT_STP_NM /* 감사단계명 */,
    A.RPTP_NM /* 보고서명 */,
    A.RPTP_KDCD /* 보고서종류코드 */,
    A.ATRZ_DMND_ID /* 결재요청아이디 */,
    A.ADT_PLAN_YR AS ADT_PLAN_YR /* 감사계획년도 */,
    A.ADT_PLAN_SQNO AS ADT_PLAN_SQNO /* 감사계획순번 */,
    A.DOC_ID /* 문서아이디 */,
    A.ATRZ_STTS_SECD /* 결재상태구분코드 */,
    F_OPEN_EDIT(A.LNKG_URL_ADDR) AS F_OPEN_EDIT
FROM TB_BADDED131M A WHERE 1=1 AND A.ATRZ_DMND_ID IS NULL (#docInfoList[].audYr#,#docInfoList[].audNo#,#docInfoList[].audStepCd#,#docInfoList[].docId#)