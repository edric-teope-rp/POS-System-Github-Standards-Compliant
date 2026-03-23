# Discount Engine API - Data Synchronization Request

## Context
The POS system and the discount-engine-api currently have separate databases. The POS database contains a `has_promotion` flag on products in the pricebook, while the discount-engine-api maintains promotion rules in the `discount_percentages` table.

Currently, there is a data synchronization issue: the POS system has 12 products marked with `has_promotion=true`, but the discount-engine-api does not have corresponding promotion configurations for these products. This causes the promotional discount feature to fail when the POS calls the API.

## Action Required
Please update the `discount_percentages` table in the discount-engine-api database to include promotion rules for all 12 UPCs listed below.

## Important: UPC Data Type
**CRITICAL:** UPCs can be of ANY length and should be stored as **STRING** type (VARCHAR or TEXT), NOT as numeric/integer types.

Some UPCs are short (e.g., "1025"), while others are long (e.g., "041594899038"). Using a numeric type would cause leading zeros to be dropped and would fail to match the POS system's UPC values.

## Promotional Products List

Below are all 12 products from the POS pricebook that have `has_promotion=true`:

| UPC | Product Name | Notes |
|-----|--------------|-------|
| 041594899038 | CIR K POLAR POP LARG | Circle K Polar Pop Large |
| 1025 | CK ICE CUP | Short numeric UPC - must be stored as string |
| 061900101136 | BELMONT KS/25S | Belmont cigarettes |
| 052000047912 | Gatoradelyte strawbe | Gatorade Lyte Strawberry |
| 817522024328 | LTX $5 SPECIAL EDTN SUPE | Lottery ticket |
| 999995306672 | COFF CUP ICE XLG TB | Coffee Cup Ice XL |
| 080660956053 | Corona Extra Beer 12 | Corona 12-pack |
| 802284004589 | EDGEF RED 100 BX | Edge Red 100 box |
| 810116120369 | PRIME HYDRATION ICE POP | Prime Hydration Ice Pop |
| 052000052862 | GTRDLYTE LMN LME ZO 20Z | Gatorade Lyte Lemon Lime Zero |
| 028400726580 | CIR K POLAR POP LARG | Circle K Polar Pop Large (duplicate?) |
| 052000328677 | GATORADE ORANGE 20Z | Gatorade Orange 20oz |

## UPCs Only (for easy copy-paste)
```
041594899038
1025
061900101136
052000047912
817522024328
999995306672
080660956053
802284004589
810116120369
052000052862
028400726580
052000328677
```

## Example Promotion Configuration
You can configure these with any promotion rules appropriate for your business logic. For example:
- Buy 2+ Get 25% Off
- Buy 3+ Get 30% Off
- Buy 5+ Get 50% Off

The key is that these UPCs MUST exist in your `discount_percentages` table for the POS promotional discount feature to work.

## Testing
After adding these UPCs to the discount_percentages table:
1. The POS system will call `/api/discounts/calculate-promotional` with these UPCs
2. The API should return `hasPromotion=true` and appropriate `discountAmount` when thresholds are met
3. The POS will display the discount and show a toast notification to the user

## Questions?
If you need more details about the promotion rules or business requirements, please coordinate with the POS team.
