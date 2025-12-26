SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.ISPCT_STR_DT /* 시찰시작일 */,
    A.ISPCT_END_DT /* 시찰종료일 */,
    A.ISPCT_STUD_SPH_NM /* 시찰수학분야명 */,
    A.TNG_ISPCT_ORG_NM /* 연수시찰기관명 */,
    A.NAT_NM /* 국가명 */,
    A.ENF_ORG_NM /* 시행기관명 */
FROM TBCSPBII322L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC