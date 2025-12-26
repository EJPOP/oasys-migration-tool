SELECT A.FMY_NAM /* 가족성명 */,
    A.FMY_EIN /* 가족암호화주민등록번호 */,
    A.FMY_RLTN_CD /* 가족관계코드 */,
    A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.RRN_SRNO /* 주민등록번호일련번호 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */
FROM TBCSPBII620L A WHERE CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# AND RRN_SRNO = #strRrnSrno#