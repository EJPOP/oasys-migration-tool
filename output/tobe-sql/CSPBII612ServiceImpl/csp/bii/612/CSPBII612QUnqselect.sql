SELECT A.UNQ_CD /* 특이사항내용 */,
    A.REL_PEN_NAM /* 관련자성명 */,
    A.REL_PEN_EIN /* 관련자주민등록번호 */,
    A.REL_PEN_RLTN_NM /* 관련자관계명 */,
    A.WRDO_SC /* 비위점수 */,
    A.UNQ_RMK /* 비고 */,
    A.UNQ_CD /* 변경이전 특이사항코드 */,
    A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.RRN_SRNO /* 주민등록번호일련번호 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */
FROM TBCSPBII630L A WHERE CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# AND RRN_SRNO = #strRrnSrno#