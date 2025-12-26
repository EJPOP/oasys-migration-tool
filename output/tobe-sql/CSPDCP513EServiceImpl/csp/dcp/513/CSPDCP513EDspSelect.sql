SELECT CLM_SECD /* 청구구분코드 */,
    CVLCPT_RCPT_YR /* 민원접수년도 */,
    CVLCPT_RCPT_RCPT /* 민원접수번호 */,
    TPOF_CYCL /* 제보차수 */,
    FRS_MDM_SQNO /* 포렌식매체순번 */,
    TPOF_DSPS_INST_CD /* 제보처분기관코드 */,
    TPOF_DSPS_INST_NM /* 제보처분기관명 */,
    DSPS_RQT_KDCD /* 처분요구종류코드 */,
    TPOF_NOPE /* 제보인원수 */,
    PNRT_PRSRV_AMT /* 추징회수보전금액 */
FROM TB_CSPDCP405L WHERE CLM_SECD = #CLM_SECD# AND CVLCPT_RCPT_YR = #CVLCPT_RCPT_YR# AND CVLCPT_RCPT_RCPT = #CVLCPT_RCPT_RCPT# AND TPOF_CYCL = #TPOF_CYCL#