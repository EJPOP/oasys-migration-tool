/* AWUSYMAMP001EMapper.findMenuInfoTreeSource 메뉴 정보 트리 정보 조회 */ SELECT MENU_ID as "id" AS MENU_ID /* 메뉴아이디 */,
    MENU_ID as "data" AS MENU_ID /* 메뉴아이디 */,
    MENU_NM as "text" AS MENU_NM /* 메뉴명 */,
    DECODE(HIRK_MNU_ID, NULL, '#', HIRK_MNU_ID) AS "parent",
    DECODE(MENU_SQNO, NULL, '9999', MENU_SQNO) AS "orderNum" AS MENU_SQNO /* 메뉴순번 */
FROM TBAWUSYMAMP001M WHERE DEL_YN = 'N' AND SYS_DVSN_CD = #{mnuId} ORDER BY MNU_SEQ ASC