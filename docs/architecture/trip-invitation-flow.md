# Trip invitation flow

```mermaid
flowchart TD
    Link["Friend opens invitation link"] --> Authenticated{"Authenticated?"}
    Authenticated -- No --> SignIn["Sign in or register"]
    SignIn --> Return["Return to original invitation"]
    Authenticated -- Yes --> Preview["Preview invitation"]
    Return --> Preview
    Preview --> Validate{"Invitation valid and uses available?"}
    Validate -- No --> Error["Specific safe error state"]
    Validate -- Yes --> Accept["Accept in one transaction"]
    Accept --> Member["Create unique TripMember"]
    Member --> Increment["Increment invite usage"]
    Increment --> Trip["Open private trip"]
```

Only a hash of the high-entropy URL-safe token is stored. Acceptance locks or atomically updates the invite row so concurrent requests cannot exceed `max_uses`. The invite grants membership only after authentication and successful validation; it never changes trip visibility.

The public preview endpoint returns only invitation-safe metadata. Creating, listing and revoking links is owner-only; accepting requires a valid Supabase JWT. The unique membership constraint is the final database guard against duplicate joins.
