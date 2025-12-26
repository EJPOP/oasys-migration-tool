/* faa/cav/043/FAACAV043EMainTotOutSelect1 */ /*총세입세출결산내역*/ SELECT ROWNUM,
    SOA_SECD /* 결산구분코드 */,
    ACNTG_CD /* 회계코드 */,
    DECODE(SUM_SE_ID,'51','월계','49','합계',ACNTG_CD||'-'|| SSEC_CD||'-'|| DITM_CD||'-'|| SITM_CD) AS SUM_SE_ID /* 합계구분아이디 */,
    ACNTG_CD /* 계정코드 */,
    CHIF_CD /* 장코드 */,
    SSEC_CD /* 관코드 */,
    DITM_CD /* 세항코드 */,
    SITM_CD /* 목코드 */,
    #N/A /* 전체결산금액1 */,
    #N/A /* 전체결산금액2 */,
    #N/A /* 전체결산금액3 */
FROM TWFAACAV061L WHERE 1=1 AND CLSG_DVSN_CD = 'A' AND ACNT_YR = #strYr# AND JRSD_CD = #JRSD_CD# AND ACNTG_CD = #ACNTG_CD# AND BLL_KDCD = #BLL_KDCD# AND PROF_RBPRSN_SQNO = #PROF_RBPRSN_SQNO# AND GVMNFC_OPER_EPS_RCDB_PBOFC_SQNO = #GVMNFC_OPER_EPS_RCDB_PBOFC_SQNO# ORDER BY KEY