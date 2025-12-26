select min(WRT_YMD) AS WRT_YMD /* 작성일자 */
from tbcspfpl005m where WRT_YMD not in( select INFO_YR || HLDY_CRTR_MND AS INFO_YR /* 정보년도 */
from TB_BADDED013B) and WRT_YMD > #strDate# AND INCD_DVSN_CD = 'A1' WRT_YMD > #strDate#