import Link from 'next/link';

const CATEGORIES = [
  { name: 'Mats', emoji: '🧘', desc: 'Non-slip, eco-friendly yoga mats' },
  { name: 'Equipment', emoji: '💪', desc: 'Blocks, straps & resistance bands' },
  { name: 'Apparel', emoji: '👕', desc: 'Breathable activewear' },
  { name: 'Accessories', emoji: '🎒', desc: 'Bags, towels & water bottles' },
  { name: 'Supplements', emoji: '🌿', desc: 'Clean protein & electrolytes' },
];

const FEATURES = [
  { icon: '🌱', title: 'Eco-Friendly', desc: 'Sustainably sourced materials' },
  { icon: '🚚', title: 'Free Shipping', desc: 'On orders over $75' },
  { icon: '↩️', title: 'Easy Returns', desc: '30-day hassle-free returns' },
  { icon: '⭐', title: 'Expert Picks', desc: 'Curated by certified instructors' },
];

export default function HomePage() {
  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-br from-emerald-700 via-teal-700 to-cyan-800 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 flex flex-col items-center text-center gap-6">
          <span className="text-6xl">🧘</span>
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight leading-tight">
            Find Your Flow
          </h1>
          <p className="text-lg md:text-xl text-emerald-100 max-w-xl">
            Premium yoga and fitness gear for every level — from your first downward dog to advanced practice.
          </p>
          <div className="flex gap-4 flex-wrap justify-center">
            <Link href="/products"
              className="bg-white text-emerald-700 font-bold px-8 py-3 rounded-xl hover:bg-emerald-50 transition-colors text-lg">
              Shop Now
            </Link>
            <Link href="/auth/register"
              className="border-2 border-white text-white font-bold px-8 py-3 rounded-xl hover:bg-white/10 transition-colors text-lg">
              Join ZenFlow
            </Link>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h2 className="text-2xl font-bold text-gray-900 mb-8 text-center">Shop by Category</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {CATEGORIES.map(cat => (
            <Link key={cat.name} href={`/products?category=${cat.name}`}
              className="bg-white rounded-2xl border border-gray-100 p-5 flex flex-col items-center text-center gap-2 hover:shadow-md hover:border-emerald-200 transition-all group">
              <span className="text-4xl group-hover:scale-110 transition-transform">{cat.emoji}</span>
              <span className="font-semibold text-gray-900">{cat.name}</span>
              <span className="text-xs text-gray-500">{cat.desc}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="bg-emerald-50 py-14">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {FEATURES.map(f => (
              <div key={f.title} className="flex flex-col items-center text-center gap-2">
                <span className="text-3xl">{f.icon}</span>
                <span className="font-semibold text-gray-900">{f.title}</span>
                <span className="text-sm text-gray-500">{f.desc}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">Ready to elevate your practice?</h2>
        <p className="text-gray-500 mb-8 max-w-md mx-auto">
          Browse our full collection of yoga mats, props, apparel and more.
        </p>
        <Link href="/products"
          className="inline-block bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-10 py-3 rounded-xl transition-colors text-lg">
          View All Products
        </Link>
      </section>
    </div>
  );
}
