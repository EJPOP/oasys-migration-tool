SELECT INFO_YR /* 정보년도 */,
    TPOF_CYCL /* 제보차수 */,
    SLCT_RPD_NM /* 청구명 */,
    SV_STR_DT,
    SV_END_DT,
    RMRK_CN /* 비고내용 */
FROM TBCSPSAE801M WHERE INFO_YR = #INFO_YR#