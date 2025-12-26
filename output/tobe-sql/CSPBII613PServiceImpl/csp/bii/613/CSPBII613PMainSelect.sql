SELECT A.SCH_DT /* 검색일 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    (SELECT B.USER_NM AS USER_NM /* 사용자명 */
FROM TBDCMACM001M B WHERE B.TRGT_NOP_ENO = A.TRGT_NOP_ENO) AS SCH_USR /* 검색사용자 */ , A.SCH_CND_NAM /* 검색조건성명 */ , A.SCH_CND_EIN /* 검색조건주민등록번호 */ , A.SCH_CND_ETC_TXT /* 검색조건기타내용 */ FROM TBCSPBII690L A /* 검색로그내역 */ ORDER BY A.SCH_DT DESC , A.FRS_MDM_SQNO DESC