SELECT INFO_YR /* 정보년도 */,
    HLDY_CRTR_MND /* 휴일기준월일 */,
    INFO_YR||HLDY_CRTR_MND AS INFO_YR /* 정보년도 */,
    HLDY_SECD /* 휴일구분코드 */,
    HLDY_NM /* 휴일명 */,
    HLDY_LNCL_YN /* 휴일음력여부 */
FROM TB_BADDED013B WHERE 1=1 AND INFO_YR||HLDY_CRTR_MND BETWEEN #strFrom# AND #strTo# /*지역코드*/ AND HLDY_SECD = #HLDY_SECD# /*지역코드*/ AND INFO_YR = #INFO_YR# /*지역코드*/