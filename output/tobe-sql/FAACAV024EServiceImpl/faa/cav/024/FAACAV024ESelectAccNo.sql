/* faa/cav/024/FAACAV024ESelectAccNo */ SELECT ACNTG_YR /* 회계년도 */,
    JRSD_CD /* 소관코드 */,
    ACNTG_CD /* 회계코드 */,
    BLL_KDCD /* 계산서종류코드 */,
    PROF_RBPRSN_SQNO /* 증명책임자순번 */,
    GVMNFC_OPER_EPS_RCDB_PBOFC_SQNO /* 관서운영경비출납공무원순번 */,
    ACTNO_SCRTY /* 계좌번호보안 */,
    ACNT_SBJCT_SECD /* 계정과목구분코드 */,
    PROF_RBPRSN_BANK_KDCD /* 증명책임자은행종류코드 */,
    PROF_RBPRSN_CRT_YMD /* 증명책임자생성일자 */,
    PROF_RBPRSN_ABL_YMD /* 증명책임자폐지일자 */
FROM TWFAACAV062L WHERE ACNTG_YR = #ACNTG_YR# AND JRSD_CD = #JRSD_CD# AND ACNTG_CD = #ACNTG_CD# AND BLL_KDCD = #BLL_KDCD# AND PROF_RBPRSN_SQNO = #PROF_RBPRSN_SQNO# AND GVMNFC_OPER_EPS_RCDB_PBOFC_SQNO = #GVMNFC_OPER_EPS_RCDB_PBOFC_SQNO#