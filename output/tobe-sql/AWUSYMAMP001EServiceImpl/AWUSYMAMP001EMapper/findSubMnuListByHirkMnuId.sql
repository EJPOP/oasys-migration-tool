/* AWUSYMAMP001EMapper.findSubMnuListByHirkMnuId 상위 메뉴로 하위 메뉴 리스트 조회 */ SELECT MENU_ID /* 메뉴아이디 */,
    SYS_DVSN_CD,
    HIRK_MNU_ID,
    MENU_NM /* 메뉴명 */,
    MNU_DES,
    MNU_URL,
    HMP_MNU_URL,
    MENU_SQNO /* 메뉴순번 */,
    ICN_RUT,
    SCR_DSPY_DVSN_CD
FROM TBAWUSYMAMP001M WHERE DEL_YN = 'N' AND HIRK_MNU_ID = #{hirkMnuId} ORDER BY MNU_SEQ ASC