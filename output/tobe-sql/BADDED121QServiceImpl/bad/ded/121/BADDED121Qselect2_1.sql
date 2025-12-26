/*쿼리명 : SQL_*/ select SITU.BZTRP_YR /* 출장년도 */,
    SITU.SPRT_NO /* 지원번호 */,
    F_CODE_NM('1000021',SITU.GNRL_BZTRP_KDCD) AS F_CODE_NM,
    /*감사종류명*/ F_DPT_INFO(SITU.BZTRP_DEPT_CD,'/*') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */
from TB_BADDED002M situ where SITU.BZTRP_YR =#BZTRP_YR# and SITU.BZTRP_SQNO = #BZTRP_SQNO#