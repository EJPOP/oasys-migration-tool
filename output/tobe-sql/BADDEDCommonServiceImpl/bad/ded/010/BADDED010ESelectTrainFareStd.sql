SELECT DST_NM /* 출발역명 */,
    ARVL_CRTR_DST_NM /* 도착기준출발역명 */,
    CRG_SECD /* 요금구분코드 */,
    TFE_KDCD /* 교통편종류코드 */,
    CRG_AMT /* 요금금액 */,
    CRG_CRTR_CN /* 요금기준내용 */,
    MDFCN_YMD /* 수정일자 */,
    CPRM_AMT /* 특별실금액 */
FROM TB_BADDED016B WHERE 1=1 AND BZTRP_SECD = #BZTRP_SECD# /* 출장구분관리코드(1:감사출장, 2:일반출장) 추가 - 2017.02.06 yyh*/ AND DST_NM LIKE #DEP_STAD_STN_NM#||'%' AND ARVL_CRTR_DST_NM LIKE #ARV_STAD_STN_NM#||'%' AND CRG_SECD = #CRG_SECD# AND TFE_KDCD = #TFE_KDCD# ORDER BY DST_NM