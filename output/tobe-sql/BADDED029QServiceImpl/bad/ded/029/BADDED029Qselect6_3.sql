SELECT TL.BZTRP_YR /* 출장년도 */,
    /* 출장년도*/ TL.BZTRP_SQNO AS BZTRP_SQNO /* 출장순번 */,
    /* 출장순번*/ TL.FT_SQNO AS FT_SQNO /* 출장조순번 */,
    /* 팀번호*/ TL.RIDE_YMD AS RIDE_YMD /* 승차일자 */,
    /* 승차일*/ F_CODE_NM('1000025',TL.TRAIN_KDCD) AS F_CODE_NM,
    /* 열차종류종류명 */ F_CODE_NM('1000026',TL.CRG_SECD) AS F_CODE_NM,
    /* 요금종류종류명 */ TL.DST_NM AS DST_NM /* 출발역명 */,
    /* 출발명*/ TL.ARVL_DST_NM AS ARVL_DST_NM /* 도착출발역명 */,
    /* 도착명*/ TL.RL_FARE_AMT AS RL_FARE_AMT /* 철도운임금액 */,
    /* 철도임*/ ( CASE WHEN TL.STD_CRG_YN = 'N' THEN '미승인' ELSE '' END) AS STD_CRG_YN /* 표준요금여부 */
FROM TB_BADDED009L tl WHERE 1 = 1 AND TL.BZTRP_YR = #BZTRP_YR# AND TL.BZTRP_SQNO = #BZTRP_SQNO# AND TO_CHAR(TL.FT_SQNO) = #TEAM_SNO#