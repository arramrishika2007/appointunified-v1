import { redirect } from 'next/navigation'

/**
 * /explore → redirect to /explore/healthcare (default sector).
 * Users can switch sectors from the sector tabs on the listing page.
 */
export default function ExplorePage() {
  redirect('/explore/healthcare')
}
