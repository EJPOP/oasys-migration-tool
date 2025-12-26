/* faa/cav/036/FAACAVOrgPopList */ SELECT A.INST_CD /* 기관코드 */,
    A.PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    A.RGN_CD /* 지역코드 */,
    A.INST_SECD /* 기관구분코드 */,
    A.SLCT_JRSD_SG_SECD /* 청탁소관부호구분코드 */,
    A.USE_DVSN_NO,
    A.RB_CD /* 소관국코드 */,
    A.PROF_RBPRSN_CPD_SECD /* 증명책임자소관과구분코드 */,
    A.OTSD_EXPRT_ADDR /* 외부전문가주소 */
FROM ( SELECT B.INST_CD /* 기관코드 */,
    B.PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    RGN_CD /* 지역코드 */,
    INST_SECD /* 기관구분코드 */,
    SLCT_JRSD_SG_SECD /* 청탁소관부호구분코드 */,
    USE_DVSN_NO,
    SUBSTR(B.ENFM_DPT_CD,1,3) AS RB_CD /* 소관국코드 */,
    SUBSTR(B.ENFM_DPT_CD,4,3) AS PROF_RBPRSN_CPD_SECD /* 증명책임자소관과구분코드 */,
    B.OTSD_EXPRT_ADDR /* 외부전문가주소 */
FROM TBDCMACM015M B LEFT OUTER JOIN (SELECT SLCT_JRSD_SG_SECD||'000000000' AS SLCT_JRSD_SG_SECD /* 청탁소관부호구분코드 */
FROM TBDCMACM015M A WHERE INST_CD LIKE '___000000000' GROUP BY SLCT_JRSD_SG_SECD) A ON (B.INST_CD = A.COPT_SG_CD_A) ) A WHERE 1=1 A.INST_CD like '%' || #strOrgCd# || '%' A.PROF_RBPRSN_INST_NM like '%' || #strOrgNm# || '%' USE_DVSN_NO = #strUseYn# /* 지정취소 된 건이 아닌것 */ AND EXISTS (SELECT DISTINCT C.PROF_RBPRSN_INST_CD AS PROF_RBPRSN_INST_CD /* 증명책임자기관코드 */
FROM TWFAACAV003M C WHERE C.MDFCN_SECD != 'D' C.INST_CD LIKE '%' || #strOrgCd# || '%' C.PROF_RBPRSN_INST_NM LIKE '%' || #strOrgNm# || '%' ) ORDER BY PROF_RBPRSN_INST_NM