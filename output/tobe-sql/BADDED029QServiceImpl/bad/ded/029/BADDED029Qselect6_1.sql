SELECT BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    REL_ADT_YR /* 관계감사년도 */,
    REL_ADT_NO /* 관계감사번호 */,
    CVLCPT_ADT_STP_SQNO /* 민원감사단계순번 */,
    SPRT_NO /* 지원번호 */
FROM TB_BADDED002M WHERE BZTRP_YR = #BZTRP_YR# AND BZTRP_SQNO = #BZTRP_SQNO#