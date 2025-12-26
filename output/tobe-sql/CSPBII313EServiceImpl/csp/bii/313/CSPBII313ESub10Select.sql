SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.RLTN_NM /* 관계명 */,
    NVL(A.FMY_EIN, A.FMY_BTDY) AS FMY_EIN /* 가족암호화주민등록번호 */,
    A.FMY_NAM /* 가족성명 */,
    A.FMY_BTDY /* 가족생일 */
FROM TBCSPBII310L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC