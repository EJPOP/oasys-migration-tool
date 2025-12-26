SELECT B.PRPS_EXEC_SLN /* 실시연번 */,
    B.MSMT_PRD_STR_DT /* 측정기간시작일 */,
    B.MSMT_PRD_END_DT /* 측정기간종료일 */,
    B.BGT_RDC_AMT /* 예산절감액 */,
    B.TRS_TX_INC_ICR_AMT /* 국고조세수입증대액 */,
    B.ADMN_IMP_EFCT_TXT /* 행정개선효과내용 */,
    A.PRPS_YE,
    A.PRPS_SLN
FROM TBCSPGSU001M A ,TBCSPGSU003D B WHERE A.PRPS_YE = B.PRPS_YE AND A.PRPS_SLN = B.PRPS_SLN AND A.PRPS_YE = #strPRPS_YE# AND A.PRPS_SLN = #strPRPS_SLN#