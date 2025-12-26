select DEPT_CD /* 부서코드 */,
    DEPT_NM /* 부서명 */
from ebaiusr.CM_DEPT where 1=1 AND DEPT_CD NOT like '%00' and USE_YN = 'Y' order by DEPT_CD