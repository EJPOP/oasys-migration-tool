SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.TRN_STR_DT /* 훈련시작일 */,
    A.TRN_END_DT /* 훈련종료일 */,
    A.TRN_DVSN_CD /* 훈련구분코드 */,
    A.TRN_CRS_NM /* 훈련과정명 */,
    A.TRN_ORG_NM /* 훈련기관명 */
FROM TBCSPBII321L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC