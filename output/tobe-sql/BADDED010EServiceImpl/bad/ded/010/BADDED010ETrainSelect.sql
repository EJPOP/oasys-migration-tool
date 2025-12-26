SELECT FT_SQNO /* 출장조순번 */,
    FT_TRGT_INST_SQNO /* 출장조대상기관순번 */,
    RL_SCO_SQNO /* 철도구간순번 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRP_YR /* 출장년도 */,
    RIDE_YMD /* 승차일자 */,
    TRAIN_KDCD /* 열차종류코드 */,
    CRG_SECD /* 요금구분코드 */,
    DST_NM /* 출발역명 */,
    ARVL_DST_NM /* 도착출발역명 */,
    RL_FARE_AMT /* 철도운임금액 */,
    STD_CRG_YN /* 표준요금여부 */,
    BGNG_DPPL_SECD /* 시작출발지구분코드 */
FROM TB_BADDED009L WHERE 1=1 AND FT_TRGT_INST_SQNO = #FT_TRGT_INST_SQNO# AND FT_SQNO = #FT_SQNO# AND BZTRP_SQNO = #BZTRP_SQNO# AND BZTRP_YR = #BZTRP_YR#